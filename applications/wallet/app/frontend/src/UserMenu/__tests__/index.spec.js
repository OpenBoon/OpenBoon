import TestRenderer, { act } from 'react-test-renderer'

import UserMenu from '..'

const noop = () => () => {}

describe('<UserMenu />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<UserMenu logout={noop} />)

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ children: 'DT' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
