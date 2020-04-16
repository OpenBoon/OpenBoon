import { colors, spacing } from '../Styles'

import Panel from '../Panel'
import Assets from '../Assets'
import Metadata from '../Metadata'

import AccountDashboardSvg from '../Icons/accountDashboard.svg'

const ICON_WIDTH = 20

const VisualizerContent = () => {
  return (
    <div
      css={{
        height: '100%',
        backgroundColor: colors.structure.coal,
        marginLeft: -spacing.spacious,
        marginRight: -spacing.spacious,
        marginBottom: -spacing.spacious,
        marginTop: spacing.hairline,
        display: 'flex',
        flex: 1,
        flexDirection: 'column',
      }}
    >
      <div css={{ display: 'flex', height: '100%', overflowY: 'hidden' }}>
        <Panel openToThe="right">
          {{
            filters: {
              title: 'Filters',
              icon: <AccountDashboardSvg width={ICON_WIDTH} aria-hidden />,
              content: '',
            },
          }}
        </Panel>
        <Assets />
        <Metadata />
      </div>
    </div>
  )
}

export default VisualizerContent
