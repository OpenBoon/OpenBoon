import { spacing, colors, typography, constants } from '../Styles'

import LockSvg from '../Icons/lock.svg'

const RoleBoundary = () => {
  return (
    <div css={{ height: '100%' }}>
      <div
        css={{
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          textAlign: 'center',
          backgroundColor: colors.structure.lead,
          boxShadow: constants.boxShadows.default,
        }}
      >
        <LockSvg width={200} />

        <h3
          css={{
            paddingTop: spacing.normal,
            fontSize: typography.size.giant,
            lineHeight: typography.height.giant,
            fontWeight: typography.weight.bold,
            maxWidth: constants.paragraph.maxWidth,
          }}
        >
          You do not have permission to access this page for this project.
        </h3>

        <p
          css={{
            fontSize: typography.size.large,
            lineHeight: typography.height.large,
            color: colors.structure.zinc,
            maxWidth: constants.paragraph.maxWidth,
          }}
        >
          Contact your project admin to be added.
        </p>
      </div>
    </div>
  )
}

export default RoleBoundary
