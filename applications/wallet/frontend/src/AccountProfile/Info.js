import { typography, spacing } from '../Styles'

import { getUser } from '../Authentication/helpers'

const AccountProfileInfo = () => {
  const { email } = getUser()

  return (
    <>
      <div
        css={{
          fontSize: typography.size.medium,
          lineHeight: typography.height.medium,
          fontWeight: typography.weight.medium,
          paddingTop: spacing.normal,
        }}>
        {`User ID: ${email}`}
      </div>
    </>
  )
}

export default AccountProfileInfo
