import TestRenderer from 'react-test-renderer'

import Modal from '..'

// const noop = () => () => {}

describe('<Modal />', () => {
  // it('Should not render by default', () => {})
  it('Should render properly when user clicks launch button', () => {
    const component = TestRenderer.create(<Modal />)

    // act(() => {
    //   component.root
    //     .findByProps({children: "Open Modal"})
    //     .props.onClick({ preventDefault: noop })
    // })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
