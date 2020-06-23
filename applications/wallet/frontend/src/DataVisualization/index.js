import Head from 'next/head'

import { colors, spacing } from '../Styles'

import Panel from '../Panel'
import Filters from '../Filters'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'

import FilterSvg from '../Icons/filter.svg'

import DataVisualizationContent from './Content'

const ICON_SIZE = 20

const DataVisualization = () => {
  return (
    <>
      <Head>
        <title>Data Visualization</title>
      </Head>

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
                icon: <FilterSvg width={ICON_SIZE} aria-hidden />,
                content: <Filters />,
              },
            }}
          </Panel>

          <SuspenseBoundary role={ROLES.ML_Tools}>
            <DataVisualizationContent />
          </SuspenseBoundary>
        </div>
      </div>
    </>
  )
}

export default DataVisualization
