import TestRenderer, { act } from 'react-test-renderer'

import AccountProfileForm from '../Form'

const noop = () => () => {}

jest.mock('../helpers')

describe('<AccountProfileForm />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<AccountProfileForm />)

    act(() => {
      component.root
        .findByProps({ id: 'firstName' })
        .props.onChange({ target: { value: 'Jane' } })
    })

    act(() => {
      component.root
        .findByProps({ id: 'lastName' })
        .props.onChange({ target: { value: 'Doe' } })
    })

    act(() => {
      component.root
        .findByProps({ type: 'submit' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
