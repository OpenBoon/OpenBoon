import { colors, spacing } from '../Styles'

import AccountDashboardSvg from '../Icons/accountDashboard.svg'

import Button, { VARIANTS } from '../Button'

const ICON_WIDTH = 20

const VisualizerPanel = () => {
  return (
    <div
      css={{
        display: 'flex',
        flexDirection: 'column',
        marginTop: spacing.hairline,
        marginRight: spacing.hairline,
      }}
    >
      <div
        css={{
          backgroundColor: colors.structure.lead,
          marginBottom: spacing.hairline,
        }}
      >
        <Button
          aria-label="Filters"
          variant={VARIANTS.NEUTRAL}
          onClick={() => {}}
          isDisabled={false}
          style={{
            padding: spacing.base,
            paddingTop: spacing.normal,
            paddingBottom: spacing.normal,
            ':hover': {
              textDecoration: 'none',
              color: colors.key.one,
              backgroundColor: colors.structure.mattGrey,
            },
          }}
        >
          <AccountDashboardSvg width={ICON_WIDTH} aria-hidden />
        </Button>
      </div>
      <div
        css={{
          flex: 1,
          backgroundColor: colors.structure.lead,
        }}
      />
    </div>
  )
}

export default VisualizerPanel
