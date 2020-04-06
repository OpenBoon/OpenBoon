import PropTypes from 'prop-types'

import { colors, spacing } from '../Styles'

import Checkbox, { VARIANTS as CHECKBOX_VARIANTS } from '../Checkbox'

import { CURRENT_POLICIES_DATE } from './helpers'

const PoliciesForm = ({ dispatch }) => {
  return (
    <>
      <p
        css={{
          margin: 0,
          paddingTop: spacing.spacious,
          color: colors.structure.zinc,
          textAlign: 'center',
          a: {
            color: colors.key.one,
          },
        }}
      >
        By accepting and clicking the &quot;Continue&quot; button you confirm
        that you have read and agree with Zorroa’s{' '}
        <a
          href={`/policies/${CURRENT_POLICIES_DATE}/terms-and-conditions.pdf`}
          target="_blank"
          rel="noopener noreferrer"
        >
          Terms&nbsp;&amp;&nbsp;Conditions
        </a>{' '}
        and{' '}
        <a
          href={`/policies/${CURRENT_POLICIES_DATE}/privacy-policy.pdf`}
          target="_blank"
          rel="noopener noreferrer"
        >
          Privacy&nbsp;Policy
        </a>
        .
      </p>

      <div
        css={{
          paddingTop: spacing.normal,
          display: 'flex',
          justifyContent: 'center',
        }}
      >
        <Checkbox
          variant={CHECKBOX_VARIANTS.PRIMARY}
          option={{
            value: 'isChecked',
            label: 'Accept',
            initialValue: false,
            isDisabled: false,
          }}
          onClick={(value) => dispatch({ isChecked: value })}
        />
      </div>
    </>
  )
}

PoliciesForm.propTypes = {
  dispatch: PropTypes.func.isRequired,
}

export { PoliciesForm as default, CURRENT_POLICIES_DATE }
