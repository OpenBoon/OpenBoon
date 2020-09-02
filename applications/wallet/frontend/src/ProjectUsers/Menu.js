import { useState } from 'react'
import PropTypes from 'prop-types'
import Link from 'next/link'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonActions from '../Button/Actions'
import Modal from '../Modal'

const ProjectUsersMenu = ({ projectId, userId, revalidate }) => {
  const [isRemoveModalOpen, setRemoveModalOpen] = useState(false)

  return (
    <>
      <Menu open="left" button={ButtonActions}>
        {({ onBlur, onClick }) => (
          <div>
            <ul>
              <li>
                <Link
                  href="/[projectId]/users/[userId]/edit"
                  as={`/${projectId}/users/${userId}/edit`}
                  passHref
                >
                  <Button
                    variant={VARIANTS.MENU_ITEM}
                    onBlur={onBlur}
                    onClick={onClick}
                  >
                    Edit
                  </Button>
                </Link>
              </li>
              <li>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onBlur={onBlur}
                  onClick={() => {
                    onClick()
                    setRemoveModalOpen(true)
                  }}
                >
                  Remove
                </Button>
              </li>
            </ul>
          </div>
        )}
      </Menu>
      {isRemoveModalOpen && (
        <Modal
          title="Remove User from Project"
          message="Are your sure you want to remove this user?"
          action="Remove User"
          onCancel={() => {
            setRemoveModalOpen(false)
          }}
          onConfirm={async () => {
            setRemoveModalOpen(false)

            await fetcher(`/api/v1/projects/${projectId}/users/${userId}/`, {
              method: 'DELETE',
            })

            revalidate()
          }}
        />
      )}
    </>
  )
}

ProjectUsersMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  userId: PropTypes.number.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default ProjectUsersMenu
