import Router from 'next/router'

export const closeSidebar = ({ setSidebarOpen }) => () => {
  const handleRouteChange = () => {
    setSidebarOpen(false)
  }

  Router.events.on('routeChangeStart', handleRouteChange)

  return () => {
    Router.events.off('routeChangeStart', handleRouteChange)
  }
}
