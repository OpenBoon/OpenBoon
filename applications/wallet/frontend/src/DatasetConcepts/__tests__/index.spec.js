import TestRenderer, { act } from 'react-test-renderer'

import datasetConcepts from '../__mocks__/datasetConcepts'

import DatasetConcepts from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const DATASET_ID = '4b0b10a8-cec1-155c-b12f-ee2bc8787e06'

describe('<DatasetConcepts />', () => {
  it('should render properly with no concepts', () => {
    require('swr').__setMockUseSWRResponse({
      data: {
        count: 0,
        next: null,
        previous: null,
        results: [],
      },
    })

    const component = TestRenderer.create(
      <DatasetConcepts projectId={PROJECT_ID} datasetId={DATASET_ID} actions />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with concepts and no actions', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({ data: datasetConcepts })

    const component = TestRenderer.create(
      <DatasetConcepts
        projectId={PROJECT_ID}
        datasetId={DATASET_ID}
        actions={false}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with concepts', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({ data: datasetConcepts })

    const component = TestRenderer.create(
      <DatasetConcepts projectId={PROJECT_ID} datasetId={DATASET_ID} actions />,
    )

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
