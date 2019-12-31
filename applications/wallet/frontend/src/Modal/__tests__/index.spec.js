import TestRenderer, { act } from 'react-test-renderer'
import Modal from '..'

const noop = () => () => {}
jest.mock('react-aria-modal', () => 'ReactAriaModal')

describe('<Modal />', () => {
  it('should render properly without data', () => {
    const component = TestRenderer.create(<Modal />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('Should render properly when user clicks open button', () => {
    const component = TestRenderer.create(<Modal />)

    act(() => {
      component.root
        .findByProps({ children: 'Open Modal' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('Should render properly when user closes modal', () => {
    const component = TestRenderer.create(<Modal />)

    act(() => {
      component.root
        .findByProps({ children: 'Open Modal' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Close Modal' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('Should render properly when user closes modal via underlay', () => {
    const component = TestRenderer.create(<Modal />)

    act(() => {
      component.root
        .findByProps({ children: 'Open Modal' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ titleId: 'Modal' })
        .props.onExit({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('Should render properly when user closes modal via close icon', () => {
    const component = TestRenderer.create(<Modal />)

    act(() => {
      component.root
        .findByProps({ children: 'Open Modal' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Close Modal' })
        .props.onKeyDown({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('Should render properly when user closes modal via cancel button', () => {
    const component = TestRenderer.create(<Modal />)

    act(() => {
      component.root
        .findByProps({ children: 'Open Modal' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ children: 'Cancel' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('Should render properly when user closes modal via delete button', () => {
    const component = TestRenderer.create(<Modal />)

    act(() => {
      component.root
        .findByProps({ children: 'Open Modal' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ children: 'Delete Permanently' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
