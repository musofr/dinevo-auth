package it.dinevo.auth;

import io.quarkus.runtime.Quarkus;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import it.dinevo.auth.dto.LoginCredentials;
import it.dinevo.auth.dto.MerchantSignupCredentials;
import it.dinevo.auth.dto.SignupCredentials;
import it.dinevo.auth.dto.VerifyOtpRequest;
import it.dinevo.auth.entity.User;
import it.dinevo.auth.entity.UserOtp;
import it.dinevo.auth.entity.UserEstablishment;
import it.dinevo.auth.exception.RateLimitException;
import it.dinevo.auth.services.OtpGenerator;
import it.dinevo.auth.services.OtpService;
import it.dinevo.auth.services.RateLimitService;
import it.dinevo.auth.services.EstablishmentCodeValidator;
import it.dinevo.auth.entity.enums.UserStatus;
import it.dinevo.auth.entity.enums.UserType;
import it.dinevo.auth.services.enums.ChannelType;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.json.JsonNumber;
import jakarta.persistence.NoResultException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.RestQuery;

import java.time.Duration;

@Path("/auth")
public class AuthResource {
    @Inject
    private JsonWebToken jwt;
    @Inject
    private OtpService otpService;
    @Inject
    private OtpGenerator otpGenerator;
    @Inject
    private RateLimitService rateLimitService;
    @Inject
    private EstablishmentCodeValidator establishmentCodeValidator;

    @POST
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> login(LoginCredentials credentials) {
        // Check rate limit before processing
        try {
            rateLimitService.checkLoginRateLimit(credentials.emailOrPhone());
        } catch (RateLimitException e) {
            return Uni.createFrom().item(
                Response.status(Response.Status.TOO_MANY_REQUESTS).build()
            );
        }

        return User
                .getUserByEmailOrPhone(credentials.emailOrPhone())
                .onItem().transformToUni(user -> {
                    if(!user.checkPassword(credentials.password())) {
                        return Uni.createFrom().item(
                            Response.status(Response.Status.UNAUTHORIZED).build()
                        );
                    }
                    // if(!user.isActive()) {
                    //     return Uni.createFrom().item(
                    //         Response.status(Response.Status.UNAUTHORIZED.getStatusCode(), "DIN_LOGIN_USER_NOT_ACTIVE").build()
                    //     );
                    // }

                    // Reset rate limit on successful login
                    rateLimitService.resetLoginAttempts(credentials.emailOrPhone());

                    return user.getEstablishmentIds()
                        .onItem().transform(establishmentIds -> {
                            var jwtBuilder = Jwt
                                .issuer("https://auth.dinevo.it")
                                .upn(user.displayName)
                                .claim("userId", user.id)
                                .claim("accountVerified", user.isVerified())
                                .groups(user.userType.name());

                            if (!establishmentIds.isEmpty()) {
                                jwtBuilder.claim("establishments", establishmentIds);
                            }
                            
                            String token = jwtBuilder
                                .expiresIn(Duration.ofHours(1))
                                .sign();
                            
                            return Response.ok().header("Authorization", "Bearer " + token).build();
                        });
                }).onFailure(NoResultException.class).recoverWithItem(() ->
                    Response.status(Response.Status.UNAUTHORIZED)
                        .entity("DIN_LOGIN_WRONG_USER_OR_PASSWORD")
                        .build()
                );
    }

    @POST
    @Path("/signup")
    public Uni<Response> signup(SignupCredentials credentials) {
        User newUser = new User();
        if((credentials.email() == null && credentials.phoneNumber() == null) || credentials.password() == null) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST.getStatusCode(), "DIN_REGISTRATION_001").build());
        }

        newUser.email = credentials.email();
        newUser.phoneNumber = credentials.phoneNumber();
        newUser.password = credentials.password();
        newUser.displayName = credentials.displayName();
        newUser.userType = UserType.CUSTOMER;

        return newUser.<User>persistAndFlush()
            .onItem().transformToUni(user -> 
                user.getEstablishmentIds()
                    .onItem().transform(establishmentIds -> {
                        var jwtBuilder = Jwt
                            .issuer("https://auth.dinevo.it")
                            .upn(user.displayName)
                            .claim("userId", user.id)
                            .claim("accountVerified", false)
                            .groups(user.userType.name());
                        
                        String token = jwtBuilder
                            .expiresIn(Duration.ofMinutes(30))
                            .sign();
                        
                        return Response.ok().header("Authorization", "Bearer " + token).build();
                    })
            );
    }

    @POST
    @Path("/register-merchant")
    public Uni<Response> signupMerchant(MerchantSignupCredentials credentials) {
        if((credentials.email() == null && credentials.phoneNumber() == null) || credentials.password() == null) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST.getStatusCode(), "DIN_REGISTRATION_001").build());
        }

        if(credentials.establishmentCode() == null || credentials.establishmentCode().isEmpty()) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST.getStatusCode(), "DIN_REGISTRATION_MERCHANT_002").build());
        }

        // Valida e consuma il codice establishment da Redis
        return establishmentCodeValidator.validateAndConsumeCode(credentials.establishmentCode())
            .onItem().transformToUni(establishmentId -> {
                if (establishmentId == null) {
                    return Uni.createFrom().item(
                        Response.status(Response.Status.BAD_REQUEST.getStatusCode(), "DIN_REGISTRATION_MERCHANT_003").build()
                    );
                }

                // Crea l'utente merchant
                User newUser = new User();
                newUser.email = credentials.email();
                newUser.phoneNumber = credentials.phoneNumber();
                newUser.password = credentials.password();
                newUser.displayName = credentials.displayName();
                newUser.userType = UserType.MERCHANT;
                newUser.userStatus = UserStatus.CONFIRMED; // I merchant sono già confermati

                return newUser.<User>persistAndFlush()
                    .onItem().transformToUni(user -> {
                        // Crea l'associazione user-establishment
                        UserEstablishment userEstablishment = new UserEstablishment();
                        userEstablishment.user = user;
                        userEstablishment.establishmentId = establishmentId.establishmentId();
                        userEstablishment.role = "OWNER";

                        return userEstablishment.persistAndFlush()
                            .onItem().transform(ue -> Response.ok().build());
                    });
            });
    }

    @POST
    @Path("/send-otp")
    @RolesAllowed({"CUSTOMER"})
    public Uni<Response> sendOtp(@RestQuery ChannelType channel) {
        Long userId = jwt.<JsonNumber>claim("userId").orElseThrow().longValue();

        return User.<User>findById(userId)
                .onItem().transformToUni(user -> {
                    // Check if user is already activated
                    if (user.userStatus != UserStatus.AWAITING_CONFIRMATION) {
                        return Uni.createFrom().item(
                            Response.status(Response.Status.BAD_REQUEST.getStatusCode(), "DIN_OTP_USER_ALREADY_ACTIVATED").build()
                        );
                    }

                    // Check rate limit
                    try {
                        rateLimitService.checkOtpRateLimit(String.valueOf(userId));
                    } catch (RateLimitException e) {
                        return Uni.createFrom().item(
                            Response.status(Response.Status.TOO_MANY_REQUESTS.getStatusCode(), "DIN_OTP_RATE_LIMIT_EXCEEDED").build()
                        );
                    }

                    return otpGenerator.getOtp()
                        .onItem().transformToUni(otp -> 
                            otpService.sendOtp(channel, user.email, otp)
                                .onItem().transformToUni(v -> UserOtp.of(user, otp))
                        )
                        .replaceWith(() -> Response.ok().build());
            });
}

    @POST
    @Path("/verify-otp")
    @RolesAllowed({"CUSTOMER"})
    public Uni<Response> verifyOtp(VerifyOtpRequest request) {
        Long userId = jwt.<JsonNumber>claim("userId").orElseThrow().longValue();

        return UserOtp.findValidOtpForUser(userId, request.otp())
                .onItem().ifNull().failWith(() -> new RuntimeException("Invalid or expired OTP"))
                .onItem().transformToUni(otp -> otp.markAsUsed()
                        .onItem().transformToUni(usedOtp -> {
                            usedOtp.user.userStatus = UserStatus.CONFIRMED;
                            return usedOtp.user.<User>persistAndFlush()
                                    .onItem().transform(user -> {
                                        var jwtBuilder = Jwt
                                                .issuer("https://auth.dinevo.it")
                                                .upn(user.displayName)
                                                .claim("userId", user.id)
                                                .claim("accountVerified", true)
                                                .groups(user.userType.name());

                                        String token = jwtBuilder
                                                .expiresIn(Duration.ofMinutes(30))
                                                .sign();

                                        return Response.ok().header("Authorization", "Bearer " + token).build();
                            });
                        }))
                .onFailure(RuntimeException.class).recoverWithItem(() ->
                    Response.status(Response.Status.BAD_REQUEST.getStatusCode(), "DIN_OTP_INVALID_OR_EXPIRED").build()
                );
    }

    public static void main(String[] args) {
        Quarkus.run(args);
    }
}


