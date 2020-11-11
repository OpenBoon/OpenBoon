import { fireEvent, render, screen } from '@testing-library/react'

import Login from '..'

describe('<Login />', () => {
  test('allows the user to log in', () => {
    render(<Login />)

    fireEvent.change(screen.getByLabelText('Username'), {
      target: { value: 'mockUsername' },
    })

    fireEvent.change(screen.getByLabelText('Password'), {
      target: { value: 'mockPassword' },
    })

    expect(screen.getAllByDisplayValue('mockUsername')).toHaveLength(1)
    expect(screen.getAllByDisplayValue('mockPassword')).toHaveLength(1)
  })
})
