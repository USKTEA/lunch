import React, { useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import styled from "styled-components";
import useAuthStore from "../hooks/useAuthStore";

const PageContainer = styled.div`
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
`;

const LoginCard = styled.div`
    background: white;
    padding: 48px;
    border-radius: 16px;
    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
    max-width: 400px;
    width: 100%;
`;

const Title = styled.h1`
    font-size: 28px;
    font-weight: 700;
    color: #1a202c;
    margin-bottom: 8px;
    text-align: center;
`;

const Subtitle = styled.p`
    font-size: 14px;
    color: #718096;
    margin-bottom: 32px;
    text-align: center;
`;

const ProviderButton = styled.button`
    width: 100%;
    padding: 16px;
    margin-bottom: 12px;
    border: none;
    border-radius: 8px;
    font-size: 16px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;

    background: ${props => props.$provider === "AZURE" ? "#0078d4" : "#4285f4"};
    color: white;

    &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }

    &:active {
        transform: translateY(0);
    }
`;

const ProviderIcon = styled.div`
    width: 24px;
    height: 24px;
    background: white;
    border-radius: 4px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: bold;
    color: ${props => props.$provider === "AZURE" ? "#0078d4" : "#4285f4"};
`;

const LoadingMessage = styled.p`
    text-align: center;
    color: #718096;
    font-size: 14px;
`;

function LoginPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const authStore = useAuthStore();
  const params = new URLSearchParams(location.search);
  const code = params.get("code");

  const defaultProviders = [
    {
      provider: "azure",
      authorizationUri: "/oauth2/authorization/azure"
    }
  ];

  // 서버에서 받은 provider와 기본 provider 합치기 (중복 제거)
  const serverProviders = authStore.providers;
  const mergedProviders = [...defaultProviders];

  serverProviders.forEach(serverProvider => {
    if (!mergedProviders.find(p => p.provider === serverProvider.provider)) {
      mergedProviders.push(serverProvider);
    } else {
      const index = mergedProviders.findIndex(p => p.provider === serverProvider.provider);
      mergedProviders[index] = serverProvider;
    }
  });



  useEffect(() => {
    const handleCodeExchange = async () => {
      if (code) {
        try {
          // AuthStore를 통해 토큰 교환
          await authStore.getToken(code, "authorization_code", "/web/login");

          // 로그인 성공 - 리다이렉트 플래그 리셋
          authStore.isRedirectingToLogin = false;
          navigate(location.pathname, { replace: true });

          // 원래 가려던 경로로 이동
          const redirectPath = authStore.getRedirectAfterLogin() || "/web/main";
          authStore.clearRedirectAfterLogin();

          navigate(redirectPath, { replace: true });
        } catch (error) {
          console.error("Token exchange failed:", error);

          navigate("/web/login", { replace: true });
        }
      }
    };

    handleCodeExchange();
  }, []);

  const handleLogin = (provider) => {
    console.log("Login with:", provider);
    // AuthStore를 통해 OAuth2 로그인 시작
    authStore.startWith(provider, "/web/login");
  };

  const getProviderDisplayName = (providerId) => {
    const names = {
      "AZURE": "Microsoft",
      "GOOGLE": "Google",
      "GITHUB": "GitHub"
    };
    return names[providerId] || providerId;
  };

  const getProviderIcon = (providerId) => {
    const icons = {
      "AZURE": "M",
      "GOOGLE": "G",
      "GITHUB": "GH"
    };
    return icons[providerId] || providerId.charAt(0);
  };

  return (
    <PageContainer>
      <LoginCard>
        <Title>로그인</Title>
        <Subtitle>계속하려면 로그인이 필요합니다</Subtitle>

        {mergedProviders.map((provider) => (
          <ProviderButton
            key={provider.provider}
            $provider={provider.provider}
            onClick={() => handleLogin(provider)}
          >
            <ProviderIcon $provider={provider.provider}>
              {getProviderIcon(provider.provider)}
            </ProviderIcon>
            {getProviderDisplayName(provider.provider)} 시작하기
          </ProviderButton>
        ))}
      </LoginCard>
    </PageContainer>
  );
}

export default LoginPage;
