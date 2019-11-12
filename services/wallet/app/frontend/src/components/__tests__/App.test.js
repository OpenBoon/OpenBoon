import React from 'react'
import { shallow } from '../../../enzymeAdapter'
import App from '../App'

describe('<App />', () => {
  it('Should render the app', () => {
    const component = shallow(<App />)
    // expect(component.text()).toBe('Hello World!')
  })
})
