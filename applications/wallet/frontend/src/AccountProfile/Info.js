import { typography, colors, spacing } from '../Styles'

import permissions from '../Permissions/__mocks__/permissions'

import { getUser } from '../Authentication/helpers'

const AccountProfileInfo = () => {
  const { email } = getUser()
  const permissionsList = permissions.results
    .map(p => p.name.replace(/([A-Z])/g, match => ` ${match}`))
    .join(', ')

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

      <div
        css={{
          color: colors.structure.steel,
          fontSize: typography.size.regular,
          lineHeight: typography.height.regular,
          fontWeight: typography.weight.regular,
        }}>
        {`Permissions: ${permissionsList}`}
      </div>
    </>
  )
}

export default AccountProfileInfo
