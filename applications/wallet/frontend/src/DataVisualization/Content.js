import { useState } from 'react'
import { useRouter } from 'next/router'

import { constants, spacing } from '../Styles'

import {
  useLocalStorageReducer,
  useLocalStorageState,
} from '../LocalStorage/helpers'

import VisualizerNavigation from '../Visualizer/Navigation'

import { reducer } from './reducer'

import DataVisualizationCreate from './Create'
import DataVisualizationActions from './Actions'
import Charts from '../Charts'

const DataVisualizationContent = () => {
  const {
    query: { projectId },
  } = useRouter()

  const [charts, dispatch] = useLocalStorageReducer({
    key: `DataVisualization.${projectId}`,
    reducer,
    initialState: [],
  })

  const [layouts, setLayouts] = useLocalStorageState({
    key: `Charts.${projectId}`,
    initialValue: {},
  })

  const [isCreating, setIsCreating] = useState(charts.length === 0)

  return (
    <div
      css={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      <VisualizerNavigation />

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
            boxShadow: constants.boxShadows.inset,
          }}
        >
          <DataVisualizationActions
            dispatch={dispatch}
            setIsCreating={setIsCreating}
            setLayouts={setLayouts}
          />

          <div css={{ flex: 1 }}>
            <Charts
              charts={charts}
              layouts={layouts}
              dispatch={dispatch}
              setLayouts={setLayouts}
            />
          </div>
        </div>
      )}
    </div>
  )
}

export default DataVisualizationContent
