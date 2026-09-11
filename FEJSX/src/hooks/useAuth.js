import { useContext } from 'react'
import { AuthContext } from '../context/contestoAuth'

export const useAuth = () => useContext(AuthContext)
