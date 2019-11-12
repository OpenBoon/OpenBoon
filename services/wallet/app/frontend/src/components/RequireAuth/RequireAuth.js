import React from 'react'
import { Route, Redirect } from 'react-router-dom'

// function RequireAuth({ component: Component, ...rest }) {
//   const { authTokens } = useAuth()
//   return (
//     <Route {...rest} render={(props) => {
//       return !!authTokens.access ? (
//         <Component {...props} />
//       ) : (
//           <Redirect to={{ pathname: '/login', state: { referer: props.location } }} />
//         )
//     }} />
//   )
// }

function RequireAuthFunc({ component: Component, ...rest }) {
  return (
    <Route {...rest} render={(props) => {
      return props.isAuthenticated ? (
        <Component {...props} />
      ) : (
          <Redirect to={{ pathname: '/login', state: { referer: props.location } }} />
        )
    }} />
  )
}

export default RequireAuthFunc