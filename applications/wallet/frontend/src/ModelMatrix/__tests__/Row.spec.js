import TestRenderer, { act } from 'react-test-renderer'

import matrix from '../__mocks__/matrix'

import ModelMatrixRow from '../Row'

const noop = () => () => {}

describe('<ModelMatrixRow />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <ModelMatrixRow
        matrix={{
          ...matrix,
          matrix: [
            [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
            [1, 9, 0, 0, 0, 0, 0, 0, 0, 0, 0],
            [2, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0],
            [3, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0],
            [4, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0],
            [5, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0],
            [6, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0],
            [7, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0],
            [8, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0],
            [9, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0],
            [10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
          ],
        }}
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
          selectedCell: [0, 0],
        }}
        label="RowLabel"
        index={0}
        dispatch={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Deselect a cell
    act(() => {
      component.root.findAllByProps({ type: 'button' })[0].props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with an absolute view', () => {
    const component = TestRenderer.create(
      <ModelMatrixRow
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
          isNormalized: false,
          selectedCell: [],
        }}
        label="RowLabel"
        index={0}
        dispatch={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Select a cell
    act(() => {
      component.root.findAllByProps({ type: 'button' })[0].props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
