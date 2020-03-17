import TestRenderer, { act } from 'react-test-renderer'

import Refresh from '../Refresh'

describe('<Refresh />', () => {
  it('should render properly', async () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <Refresh onClick={mockFn} assetType="Stuff" />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ variant: 'PRIMARY_SMALL' }).props.onClick()
    })

    expect(mockFn).toHaveBeenCalled()
    expect(component.toJSON()).toMatchSnapshot()
  })
})
