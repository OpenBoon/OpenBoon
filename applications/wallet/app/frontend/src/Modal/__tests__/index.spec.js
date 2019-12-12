import TestRenderer, { act } from 'react-test-renderer'
import Modal from '..'

const noop = () => () => {}
jest.mock('react-aria-modal')

describe('<Modal />', () => {
  // it('Should not render by default', () => {})
  it('Should render properly when user clicks launch button', () => {
    const component = TestRenderer.create(<Modal />)

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ children: 'Open Modal' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
