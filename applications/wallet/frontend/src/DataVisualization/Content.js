import { useRouter } from 'next/router'
import useSWR from 'swr'

import { colors, spacing } from '../Styles'

import { cleanup } from '../Filters/helpers'

import Panel from '../Panel'
import Filters from '../Filters'
import VisualizerNavigation from '../Visualizer/Navigation'

import FilterSvg from '../Icons/filter.svg'

import DataVisualizationCreate from './Create'

const ICON_WIDTH = 20
const FROM = 0
const SIZE = 100

const DataVisualizationContent = () => {
  const {
    query: { projectId, query },
  } = useRouter()

  const q = cleanup({ query })

  const {
    data: { count: itemCount },
  } = useSWR(
    `/api/v1/projects/${projectId}/searches/query/?query=${q}&from=${FROM}&size=${SIZE}`,
  )

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

        <div css={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
          {!!itemCount && <VisualizerNavigation itemCount={itemCount} />}

          <DataVisualizationCreate />
        </div>
      </div>
    </div>
  )
}

export default DataVisualizationContent
