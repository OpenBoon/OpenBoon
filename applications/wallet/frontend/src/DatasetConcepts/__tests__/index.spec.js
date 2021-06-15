import TestRenderer, { act } from 'react-test-renderer'

import datasetConcepts from '../__mocks__/datasetConcepts'

import DatasetConcepts from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const DATASET_ID = '621bf775-89d9-1244-9596-d6df43f1ede5'

describe('<DatasetConcepts />', () => {
  it('should render properly with no concepts', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/datasets/[datasetId]',
      query: { projectId: PROJECT_ID, datasetId: DATASET_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {
        count: 0,
        next: null,
        previous: null,
        results: [],
      },
    })

    const component = TestRenderer.create(<DatasetConcepts />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with concepts', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/datasets/[datasetId]',
      query: { projectId: PROJECT_ID, datasetId: DATASET_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: datasetConcepts })

    const component = TestRenderer.create(<DatasetConcepts />)

    expect(component.toJSON()).toMatchSnapshot()

    // Open menu
    act(() => {
      component.root
        .findAllByProps({ 'aria-label': 'Toggle Actions Menu' })[0]
        .props.onClick()
    })

    // Click Edit
    act(() => {
      component.root.findByProps({ children: 'Edit' }).props.onClick()
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/datasets/[datasetId]?edit=${datasetConcepts.results[0].label}`,
      `/${PROJECT_ID}/datasets/${DATASET_ID}`,
    )
  })
})
