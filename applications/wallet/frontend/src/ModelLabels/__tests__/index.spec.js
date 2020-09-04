import TestRenderer, { act } from 'react-test-renderer'

import modelLabels from '../__mocks__/modelLabels'

import ModelLabels from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = '621bf775-89d9-1244-9596-d6df43f1ede5'

describe('<ModelLabels />', () => {
  it('should render properly with no labels', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]',
      query: { projectId: PROJECT_ID, modelId: MODEL_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {
        count: 0,
        next: null,
        previous: null,
        results: [],
      },
    })

    const component = TestRenderer.create(
      <ModelLabels requiredAssetsPerLabel={10} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with labels', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]',
      query: { projectId: PROJECT_ID, modelId: MODEL_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: modelLabels })

    const component = TestRenderer.create(
      <ModelLabels requiredAssetsPerLabel={10} />,
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
      `/[projectId]/models/[modelId]?edit=${modelLabels.results[0].label}`,
      `/${PROJECT_ID}/models/${MODEL_ID}`,
    )
  })

  it('should render properly with required assets per labels', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]',
      query: { projectId: PROJECT_ID, modelId: MODEL_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: modelLabels })

    const component = TestRenderer.create(
      <ModelLabels requiredAssetsPerLabel={1} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
