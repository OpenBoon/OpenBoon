import TestRenderer, { act } from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'
import datasetModels from '../__mocks__/datasetModels'
import modelTypes from '../../ModelTypes/__mocks__/modelTypes'

import User from '../../User'

import DatasetModelsTable from '../Table'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const DATASET_ID = '621bf775-89d9-1244-9596-d6df43f1ede5'

describe('<DatasetModelsTable />', () => {
  it('should render properly without models', async () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/datasets/[datasetId]/models',
      query: { projectId: PROJECT_ID, datasetId: DATASET_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: {} })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <DatasetModelsTable
          projectId={PROJECT_ID}
          datasetId={DATASET_ID}
          modelTypes={modelTypes.results}
        />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with models', async () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/datasets/[datasetId]/models',
      query: { projectId: PROJECT_ID, datasetId: DATASET_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: datasetModels })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <DatasetModelsTable
          projectId={PROJECT_ID}
          datasetId={DATASET_ID}
          modelTypes={modelTypes.results}
        />
      </User>,
    )

    // Open Menu
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Actions Menu' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
