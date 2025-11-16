import React, { useEffect } from 'react';
import { BrowserRouter, Routes, Route, useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import { authStore } from './stores/AuthStore';
import Header from './components/layout/Header';
import Footer from './components/layout/Footer';
import MainPage from './pages/MainPage';
import LoginPage from './pages/LoginPage';
import useAuthStore  from "./hooks/useAuthStore";
import useUserStore from "./hooks/useUserStore";

const AppContainer = styled.div`
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
`;

const Main = styled.main`
  flex: 1;
  overflow: hidden;
`;

// AuthStore에 navigate 함수를 주입하는 컴포넌트
function NavigateInjector() {
  const navigate = useNavigate();

  useEffect(() => {
    authStore.setNavigate(navigate);
  }, [navigate]);

  return null;
}

const App = () => {
  const authStore = useAuthStore();
  const userStore = useUserStore();

  useEffect(() => {
    authStore.loadToken()
    userStore.fetchUser()
  }, []);

  return (
    <BrowserRouter>
      <NavigateInjector />
      <AppContainer>
        <Header />
        <Main>
          <Routes>
            <Route path="/web/main" element={<MainPage />} />
            <Route path="/web/login" element={<LoginPage />} />
          </Routes>
        </Main>
        <Footer />
      </AppContainer>
    </BrowserRouter>
  );
};

export default App;
