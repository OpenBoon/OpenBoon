import { spacing } from '../Styles'

import Loading from '../Loading'

const AuthenticationLoading = () => {
  return (
    <div css={{ height: '100vh', display: 'flex', padding: spacing.spacious }}>
      <Loading />
    </div>
  )
}

export default AuthenticationLoading
