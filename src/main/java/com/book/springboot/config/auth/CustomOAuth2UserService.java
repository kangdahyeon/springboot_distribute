package com.book.springboot.config.auth;

import com.book.springboot.config.auth.dto.OAuthAttributes;
import com.book.springboot.config.auth.dto.SessionUser;

import com.book.springboot.domain.user.User;
import com.book.springboot.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final HttpSession httpSession;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2UserService delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);


        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("registrationId {}", registrationId);
        // registrationId : 현재 로그인 진행 중인 서비스를 구분하는 코드. 네이버, 구글 등 소셜로그인이 여러개일때 사용.
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();
        // userNameAttributeName : OAuth2 로그인 진행 시 키가 되는 필드값을 이야기함. primaryKey와 같은 의미.
        // 구글의 경우 기본적으로 코드를 지원(기본코드 sub). 네이버/카카오는 지원 안함

        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());
        // OAuthAttributes : OAuth2UserService를 통해 가져운 OAuth2User의 attribute를 담을 클래스.
        User user = saveOrUpdate(attributes);

        httpSession.setAttribute("user", new SessionUser(user));
        log.info("user{}", user.getName());

        // SessionUser : 세션에 사용자 정보를 저장하기 위한 Dto클래스.

        return new DefaultOAuth2User(Collections.singleton(new SimpleGrantedAuthority(user.getRoleKey())),
                attributes.getAttributes(), attributes.getNameAttributeKey());
    }

    private User saveOrUpdate(OAuthAttributes attributes) {

        log.info("attributes: {}", attributes);
        User user = userRepository.findByEmail(attributes.getEmail())
                .map(entity -> entity.update(attributes.getName(), attributes.getPicture()))
                .orElse(attributes.toEntity());

        return userRepository.save(user);
    }
}
