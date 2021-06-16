import TestRenderer from 'react-test-renderer'

import modelTypes from '../../ModelTypes/__mocks__/modelTypes'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import DatasetModels from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const DATASET_ID = '4b0b10a8-cec1-155c-b12f-ee2bc8787e06'

jest.mock('../Table', () => 'DatasetModelsTable')

describe('<DatasetModels />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/datasets/[datasetId]/models',
      query: {
        projectId: PROJECT_ID,
        datasetId: DATASET_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({ data: modelTypes })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <DatasetModels />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
