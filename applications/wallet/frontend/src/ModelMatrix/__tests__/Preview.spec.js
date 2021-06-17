import TestRenderer, { act } from 'react-test-renderer'

import assets from '../../Assets/__mocks__/assets'

import matrix from '../__mocks__/matrix'

import { MIN_WIDTH as PANEL_MIN_WIDTH } from '../../Panel/helpers'

import ModelMatrixPreview from '../Preview'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = '621bf775-89d9-1244-9596-d6df43f1ede5'

jest.mock('next/link', () => 'Link')

const noop = () => () => {}

describe('ModelMatrixPreview', () => {
  it('should render properly with selection', async () => {
    require('next/router').__setUseRouter({
      query: {
        projectId: PROJECT_ID,
        modelId: MODEL_ID,
      },
    })

    require('swr').__setMockUseSWRInfiniteResponse({ data: [assets] })

    const component = TestRenderer.create(
      <ModelMatrixPreview
        settings={{
          selectedCell: [0, 1],
          minScore: 0,
          maxScore: 1,
        }}
        labels={matrix.labels}
        moduleName={matrix.moduleName}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ children: 'Load More' }).props.onClick()
    })

    // eslint-disable-next-line no-proto
    const spy = jest.spyOn(localStorage.__proto__, 'setItem')

    await act(async () => {
      component.root
        .findByProps({ 'aria-label': 'View Filter Panel' })
        .props.onClick({
          preventDefault: noop,
          stopPropagation: noop,
        })
    })

    expect(spy).toHaveBeenCalledWith(
      'rightOpeningPanelSettings',
      JSON.stringify({
        size: PANEL_MIN_WIDTH,
        isOpen: true,
        openPanel: 'filters',
      }),
    )
  })

  it('should render properly when there are no predicted assets', () => {
    require('next/router').__setUseRouter({
      query: {
        projectId: PROJECT_ID,
        modelId: MODEL_ID,
      },
    })

    require('swr').__setMockUseSWRInfiniteResponse({
      data: [{ count: 0, results: [] }],
    })

    const component = TestRenderer.create(
      <ModelMatrixPreview
        settings={{
          selectedCell: [0, 1],
          minScore: 0,
          maxScore: 1,
        }}
        labels={matrix.labels}
        moduleName={matrix.moduleName}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly without data', () => {
    require('next/router').__setUseRouter({
      query: {
        projectId: PROJECT_ID,
        modelId: MODEL_ID,
      },
    })

    require('swr').__setMockUseSWRInfiniteResponse({})

    const component = TestRenderer.create(
      <ModelMatrixPreview
        settings={{
          selectedCell: [0, 1],
          minScore: 0,
          maxScore: 1,
        }}
        labels={matrix.labels}
        moduleName={matrix.moduleName}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with an error', () => {
    require('next/router').__setUseRouter({
      query: {
        projectId: PROJECT_ID,
        modelId: MODEL_ID,
      },
    })

    require('swr').__setMockUseSWRInfiniteResponse({ error: 'error' })

    const component = TestRenderer.create(
      <ModelMatrixPreview
        settings={{
          selectedCell: [0, 1],
          minScore: 0,
          maxScore: 1,
        }}
        labels={matrix.labels}
        moduleName={matrix.moduleName}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly without selection', () => {
    require('next/router').__setUseRouter({
      query: {
        projectId: PROJECT_ID,
        modelId: MODEL_ID,
      },
    })

    require('swr').__setMockUseSWRInfiniteResponse({ data: [assets] })

    const component = TestRenderer.create(
      <ModelMatrixPreview
        settings={{
          selectedCell: [],
          minScore: 0,
          maxScore: 1,
        }}
        labels={matrix.labels}
        moduleName={matrix.moduleName}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
