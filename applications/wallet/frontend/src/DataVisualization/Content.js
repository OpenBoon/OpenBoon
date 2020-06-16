import { colors, spacing } from '../Styles'

import Panel from '../Panel'
import Filters from '../Filters'

import FilterSvg from '../Icons/filter.svg'

const ICON_WIDTH = 20

const DataVisualizationContent = () => {
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
      </div>
    </div>
  )
}

export default DataVisualizationContent
