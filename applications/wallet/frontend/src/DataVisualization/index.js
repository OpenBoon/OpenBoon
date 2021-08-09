import Head from 'next/head'
import { useRouter } from 'next/router'

import { colors, spacing } from '../Styles'

import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'
import FetchAhead from '../Fetch/Ahead'
import Panel from '../Panel'
import FiltersIcon from '../Filters/Icon'
import Filters from '../Filters'

import DataVisualizationContent from './Content'

const DataVisualization = () => {
  const {
    query: { projectId },
  } = useRouter()

  return (
    <>
      <Head>
        <title>Data Visualization</title>
      </Head>

      <SuspenseBoundary role={ROLES.ML_Tools}>
        <FetchAhead url={`/api/v1/projects/${projectId}/searches/fields/`} />
        <FetchAhead url={`/api/v1/projects/${projectId}/datasets/all/`} />

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
