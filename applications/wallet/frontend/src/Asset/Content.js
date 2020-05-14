import { colors, spacing, constants } from '../Styles'

import Panel from '../Panel'
import Metadata from '../Metadata'

import InformationSvg from '../Icons/information.svg'

const ICON_WIDTH = 20

const AssetContent = () => {
  return (
    <div
      css={{
        height: '100%',
        backgroundColor: colors.structure.coal,
        marginLeft: -spacing.spacious,
        marginRight: -spacing.spacious,
        marginBottom: -spacing.spacious,
        paddingTop: spacing.hairline,
        display: 'flex',
        flex: 1,
        flexDirection: 'column',
      }}
    >
      <div css={{ display: 'flex', height: '100%', overflowY: 'hidden' }}>
        <div
          css={{
            flex: 1,
            position: 'relative',
            marginBottom: -spacing.mini,
            boxShadow: constants.boxShadows.assets,
          }}
        >
          Yo!
        </div>
        <Panel openToThe="left">
          {{
            metadata: {
              title: 'Asset Metadata',
              icon: <InformationSvg width={ICON_WIDTH} aria-hidden />,
              content: <Metadata />,
            },
          }}
        </Panel>
      </div>
    </div>
  )
}

export default AssetContent
