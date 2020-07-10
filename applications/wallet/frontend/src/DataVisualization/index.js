import Head from 'next/head'

import { colors, spacing } from '../Styles'

import Panel from '../Panel'
import Filters from '../Filters'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'
import FiltersIcon from '../Filters/Icon'

import DataVisualizationContent from './Content'

const DataVisualization = () => {
  return (
    <>
      <Head>
        <title>Data Visualization</title>
      </Head>

      <SuspenseBoundary role={ROLES.ML_Tools}>
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
                  icon: <FiltersIcon />,
                  content: <Filters />,
                },
              }}
            </Panel>

            <SuspenseBoundary>
              <DataVisualizationContent />
            </SuspenseBoundary>
          </div>
        </div>
      </SuspenseBoundary>
    </>
  )
}

export default DataVisualization
