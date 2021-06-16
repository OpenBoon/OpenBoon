import TestRenderer from 'react-test-renderer'

import dataset from '../__mocks__/dataset'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import Dataset from '..'

jest.mock('../../Breadcrumbs', () => 'Breadcrumbs')
jest.mock('../../DatasetLabels', () => 'DatasetLabels')
jest.mock('../../DatasetModels', () => 'DatasetModels')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const DATASET_ID = '621bf775-89d9-1244-9596-d6df43f1ede5'

describe('<Dataset />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/datasets/[datasetId]',
      query: {
        projectId: PROJECT_ID,
        datasetId: DATASET_ID,
        action: 'edit-concept-success',
      },
    })

    require('swr').__setMockUseSWRResponse({ data: dataset })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Dataset />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly in edit concept mode', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/datasets/[datasetId]',
      query: {
        projectId: PROJECT_ID,
        datasetId: DATASET_ID,
        action: 'delete-concept-success',
        edit: 'cat',
      },
    })

    require('swr').__setMockUseSWRResponse({ data: dataset })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Dataset />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should route properly to labels', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/datasets/[datasetId]/labels',
      query: {
        projectId: PROJECT_ID,
        datasetId: DATASET_ID,
        action: 'remove-asset-success',
      },
    })

    require('swr').__setMockUseSWRResponse({ data: dataset })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Dataset />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should route properly to models', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/datasets/[datasetId]/models',
      query: {
        projectId: PROJECT_ID,
        datasetId: DATASET_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({ data: dataset })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Dataset />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
