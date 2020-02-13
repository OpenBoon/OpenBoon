import PropTypes from 'prop-types'

import { spacing, colors, constants } from '../Styles'

import CheckboxTableRow from './TableRow'

const CheckboxTable = ({ options, onClick }) => {
  return (
    <table
      cellSpacing="0"
      css={{
        width: '100%',
        color: colors.structure.white,
        textAlign: 'left',
        'th, td': {
          padding: spacing.normal,
          paddingLeft: 0,
          ':first-of-type': {
            paddingLeft: spacing.normal,
          },
        },
        td: {
          borderTop: constants.borders.tabs,
          ':first-of-type': {
            border: 'none',
          },
        },
        tr: {
          ':hover': {
            td: {
              backgroundColor: colors.structure.iron,
            },
            '+ tr': {
              td: {
                borderTopColor: 'transparent',
              },
            },
          },
        },
        'tr:last-of-type': {
          td: {
            borderBottom: constants.borders.tabs,
            ':first-of-type': {
              border: 'none',
            },
          },
        },
      }}>
      <thead>
        <tr>
          <th>&nbsp;</th>
          <th>Name</th>
          <th>Description</th>
        </tr>
      </thead>
      <tbody>
        {options.map(option => (
          <CheckboxTableRow
            key={option.key}
            value={option.key}
            label={option.label}
            icon={option.icon}
            legend={option.legend}
            initialValue={false}
            onClick={value => onClick({ [option.key]: value })}
          />
        ))}
      </tbody>
    </table>
  )
}

CheckboxTable.propTypes = {
  options: PropTypes.arrayOf(
    PropTypes.shape({
      key: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
      legend: PropTypes.string.isRequired,
    }),
  ).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default CheckboxTable
