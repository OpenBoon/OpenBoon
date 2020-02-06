import TestRenderer, { act } from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'

import AccountProfileForm from '../Form'

jest.mock('../../Authentication/helpers', () => ({
  getUser: () => mockUser,
}))

jest.mock('../helpers')

const noop = () => () => {}

describe('<AccountProfileForm />', () => {
  it('should render properly when a user edits username', () => {
    const component = TestRenderer.create(<AccountProfileForm />)

    expect(component.toJSON()).toMatchSnapshot()

    // Click edit username
    act(() => {
      component.root
        .findByProps({ children: 'Edit Username' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

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

    // Submit the form
    act(() => {
      component.root
        .findByProps({ children: 'Save' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when a user cancels', () => {
    const component = TestRenderer.create(<AccountProfileForm />)

    // Click edit username
    act(() => {
      component.root
        .findByProps({ children: 'Edit Username' })
        .props.onClick({ preventDefault: noop })
    })

    // Click cancel
    act(() => {
      component.root
        .findByProps({ children: 'Cancel' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
