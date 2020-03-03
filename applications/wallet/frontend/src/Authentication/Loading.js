import { spacing } from '../Styles'

import Loading from '../Loading'

const AuthenticationLoader = () => {
  return (
    <div css={{ height: '100vh', display: 'flex' }}>
      <div css={{ flex: 1, display: 'flex', padding: spacing.spacious }}>
        <Loading />
      </div>
    </div>
  )
}

export default AuthenticationLoader
