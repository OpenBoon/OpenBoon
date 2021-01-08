import TestRenderer, { act } from 'react-test-renderer'

import matrix from '../__mocks__/matrix'

import ModelMatrixMatrix from '../Matrix'

const noop = () => () => {}

describe('<ModelMatrixMatrix />', () => {
  it('should render properly', () => {
    require('swr').__setMockUseSWRResponse({ data: matrix })

    const component = TestRenderer.create(
      <ModelMatrixMatrix
        settings={{
          minScore: 0,
          maxScore: 1,
          isPreviewOpen: false,
          width: 100,
          height: 100,
          labelsWidth: 100,
          zoom: 1,
          isMinimapOpen: true,
          isNormalized: true,
          selectedCell: [],
        }}
        dispatch={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Hide Minimap
    act(() => {
      component.root.findByProps({ 'aria-label': 'Mini map' }).props.onClick()
    })

    // Zoom 2x
    act(() => {
      component.root.findByProps({ 'aria-label': 'Zoom In' }).props.onClick()
    })

    // Back to zoom 1x
    act(() => {
      component.root.findByProps({ 'aria-label': 'Zoom Out' }).props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with Preview', () => {
    require('swr').__setMockUseSWRResponse({ data: matrix })

    const component = TestRenderer.create(
      <ModelMatrixMatrix
        settings={{
          minScore: 0,
          maxScore: 1,
          isPreviewOpen: true,
          width: 100,
          height: 100,
          labelsWidth: 100,
          zoom: 1,
          isMinimapOpen: true,
          isNormalized: true,
          selectedCell: [],
        }}
        dispatch={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
