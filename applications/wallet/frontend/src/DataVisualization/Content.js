import { useState } from 'react'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { spacing } from '../Styles'

import { cleanup } from '../Filters/helpers'
import { useLocalStorageReducer } from '../LocalStorage/helpers'

import VisualizerNavigation from '../Visualizer/Navigation'

import { reducer } from './reducer'

import DataVisualizationCreate from './Create'
import DataVisualizationActions from './Actions'
import Charts from '../Charts'

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

  const [charts, dispatch] = useLocalStorageReducer({
    key: `DataVisualization.${projectId}`,
    reducer,
    initialState: [],
  })

  const [isCreating, setIsCreating] = useState(charts.length === 0)

  return (
    <div css={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
      {!!itemCount && <VisualizerNavigation itemCount={itemCount} />}

      {isCreating ? (
        <DataVisualizationCreate
          charts={charts}
          dispatch={dispatch}
          setIsCreating={setIsCreating}
        />
      ) : (
        <div
          css={{
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
            padding: spacing.normal,
            overflow: 'auto',
          }}
        >
          <DataVisualizationActions
            dispatch={dispatch}
            setIsCreating={setIsCreating}
          />

          <div css={{ flex: 1 }}>
            <Charts projectId={projectId} charts={charts} dispatch={dispatch} />
          </div>
        </div>
      )}
    </div>
  )
}

export default DataVisualizationContent
