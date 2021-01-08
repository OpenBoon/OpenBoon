import TestRenderer, { act } from 'react-test-renderer'

import matrix from '../__mocks__/matrix'

import ModelMatrixMinimap from '../Minimap'

describe('<ModelMatrixMinimap />', () => {
  it('should render properly', () => {
    require('swr').__setMockUseSWRResponse({ data: matrix })

    const component = TestRenderer.create(
      <ModelMatrixMinimap
        matrix={matrix}
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
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.unmount()
    })
  })

  it('should render properly when Minimap is hidden', () => {
    require('swr').__setMockUseSWRResponse({ data: matrix })

    const component = TestRenderer.create(
      <ModelMatrixMinimap
        matrix={matrix}
        settings={{
          minScore: 0,
          maxScore: 1,
          isPreviewOpen: false,
          width: 100,
          height: 100,
          labelsWidth: 100,
          zoom: 1,
          isMinimapOpen: false,
          isNormalized: true,
          selectedCell: [],
        }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
