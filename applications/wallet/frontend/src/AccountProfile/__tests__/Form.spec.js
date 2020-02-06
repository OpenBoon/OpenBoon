import TestRenderer, { act } from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'

import AccountProfileForm from '../Form'

const noop = () => () => {}

jest.mock('../../Authentication/helpers')
jest.mock('../helpers')

describe('<AccountProfileForm />', () => {
  it('should render properly when a user edits username', () => {
    require('../../Authentication/helpers').__setMockUser(mockUser)

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

    require('../../Authentication/helpers').__setMockUser(mockUser)

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
