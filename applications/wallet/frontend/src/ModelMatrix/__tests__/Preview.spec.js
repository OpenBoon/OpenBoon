import TestRenderer, { act } from 'react-test-renderer'

import matrix from '../__mocks__/matrix'

import ModelMatrixPreview from '../Preview'

describe('<ModelMatrixPreview />', () => {
  it('should render properly', () => {
    require('swr').__setMockUseSWRResponse({
      data: matrix,
    })

    const component = TestRenderer.create(
      <ModelMatrixPreview matrix={matrix} selectedCell={[0, 1]} />,
    )

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'View Filter Panel' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with no selected cells', () => {
    require('swr').__setMockUseSWRResponse({
      data: matrix,
    })

    const component = TestRenderer.create(
      <ModelMatrixPreview matrix={matrix} selectedCell={[]} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
