package com.berdachuk.medexpertmatch.core.service.impl;

import com.berdachuk.medexpertmatch.core.domain.ApiSessionToken;
import com.berdachuk.medexpertmatch.core.domain.ApiSessionTokenCreated;
import com.berdachuk.medexpertmatch.core.domain.ApiSessionTokenView;
import com.berdachuk.medexpertmatch.core.domain.RateLimitTier;
import com.berdachuk.medexpertmatch.core.repository.ApiSessionTokenRepository;
import com.berdachuk.medexpertmatch.core.service.ApiSessionTokenAdminService;
import com.berdachuk.medexpertmatch.core.util.ApiKeyMasker;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ApiSessionTokenAdminServiceImpl implements ApiSessionTokenAdminService {

    private final ApiSessionTokenRepository apiSessionTokenRepository;

    public ApiSessionTokenAdminServiceImpl(ApiSessionTokenRepository apiSessionTokenRepository) {
        this.apiSessionTokenRepository = apiSessionTokenRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiSessionTokenView> listTokens() {
        return apiSessionTokenRepository.findAll().stream().map(this::toView).toList();
    }

    @Override
    @Transactional
    public ApiSessionTokenCreated createToken(String description, RateLimitTier tier, Instant expiresAt) {
        String apiKey = UUID.randomUUID().toString().replace("-", "");
        Instant now = Instant.now();
        ApiSessionToken token = new ApiSessionToken(
                IdGenerator.generateId(),
                apiKey,
                description != null ? description : "",
                tier != null ? tier : RateLimitTier.DEFAULT,
                expiresAt,
                now,
                null);
        apiSessionTokenRepository.insert(token);
        return new ApiSessionTokenCreated(
                token.id(),
                apiKey,
                ApiKeyMasker.prefix(apiKey),
                token.description(),
                token.rateLimitTier(),
                token.expiresAt(),
                token.createdAt());
    }

    @Override
    @Transactional
    public boolean revokeToken(String id) {
        return apiSessionTokenRepository.deleteById(id);
    }

    private ApiSessionTokenView toView(ApiSessionToken token) {
        return new ApiSessionTokenView(
                token.id(),
                ApiKeyMasker.prefix(token.apiKey()),
                token.description(),
                token.rateLimitTier(),
                token.expiresAt(),
                token.createdAt(),
                token.lastUsedAt());
    }
}
