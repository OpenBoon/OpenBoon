import React from 'react'
import { shallow } from '../../enzyme'
import Wallet from '../Wallet'

describe('<Wallet />', () => {
  it('Should render the app', () => {
    const component = shallow(<Wallet />)
    expect(component.text()).toBe('Hello World!')
  })
})