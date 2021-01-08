import TestRenderer from 'react-test-renderer'

import matrix from '../__mocks__/matrix'

import ModelMatrixResize from '../Resize'

jest.mock('../Minimap', () => 'ModelMatrixMinimap')

const noop = () => () => {}

describe('<ModelMatrixResize />', () => {
  it('should render properly', () => {
    require('swr').__setMockUseSWRResponse({
      data: matrix,
    })

    const component = TestRenderer.create(
      <ModelMatrixResize
        matrix={matrix}
        settings={{ zoom: 1, isMinimapOpen: false }}
        dispatch={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
