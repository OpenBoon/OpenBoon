import { typography, colors, spacing } from '../Styles'

const AccountProfileInfo = () => {
  return (
    <>
      <div
        css={{
          fontSize: typography.size.medium,
          lineHeight: typography.height.medium,
          fontWeight: typography.weight.medium,
          paddingTop: spacing.normal,
        }}>
        User ID: Danny@Zorroa.com
      </div>

      <div
        css={{
          color: colors.structure.steel,
          fontSize: typography.size.regular,
          lineHeight: typography.height.regular,
          fontWeight: typography.weight.regular,
        }}>
        Permissions: User Admin, Job Management, API Key Management
      </div>
    </>
  )
}

export default AccountProfileInfo
