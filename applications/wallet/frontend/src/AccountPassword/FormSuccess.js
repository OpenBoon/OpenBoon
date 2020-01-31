import PropTypes from 'prop-types'

import { spacing } from '../Styles'

import FormSuccess from '../FormSuccess'
import Button, { VARIANTS } from '../Button'

const AccountPasswordFormSuccess = ({ onReset }) => {
  return (
    <div>
      <FormSuccess>New Password Saved!</FormSuccess>
      <div css={{ display: 'flex', paddingTop: spacing.normal }}>
        <Button variant={VARIANTS.PRIMARY} onClick={onReset}>
          Edit Password Again
        </Button>
      </div>
    </div>
  )
}

AccountPasswordFormSuccess.propTypes = {
  onReset: PropTypes.func.isRequired,
}

export default AccountPasswordFormSuccess
