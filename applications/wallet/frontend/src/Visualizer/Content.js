import { colors, spacing } from '../Styles'

import Panel from '../Panel'
import Assets from '../Assets'
import Filters from '../Filters'
import Metadata from '../Metadata'
import Export from '../Export'

import FilterSvg from '../Icons/filter.svg'
import InformationSvg from '../Icons/information.svg'
import UploadSvg from '../Icons/upload.svg'

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
        paddingTop: spacing.hairline,
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
              icon: <FilterSvg width={ICON_WIDTH} aria-hidden />,
              content: <Filters />,
            },
          }}
        </Panel>
        <Assets />
        <Panel openToThe="left">
          {{
            metadata: {
              title: 'Asset Metadata',
              icon: <InformationSvg width={ICON_WIDTH} aria-hidden />,
              content: <Metadata />,
            },
            export: {
              title: 'Export',
              icon: (
                <UploadSvg
                  width={ICON_WIDTH}
                  css={{ transform: `rotate(180deg)` }}
                  aria-hidden
                />
              ),
              content: <Export />,
            },
          }}
        </Panel>
      </div>
    </div>
  )
}

export default VisualizerContent
